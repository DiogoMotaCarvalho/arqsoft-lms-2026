def settings(String environment) {
    if (!(environment in ['dev', 'pp', 'prod'])) {
        error('Unknown environment.')
    }
    def target = env.PSOFT_SSH_TARGET
    def port = env.PSOFT_SSH_PORT ?: '22'
    def url
    switch (environment) {
        case 'dev':
            url = env.PSOFT_DEV_URL
            break
        case 'pp':
            url = env.PSOFT_PP_URL
            break
        case 'prod':
            url = env.PSOFT_PROD_URL
            break
    }
    if (!target || !(target ==~ /[a-zA-Z0-9_.-]+@[a-zA-Z0-9_.-]+/) ||
            !(port ==~ /[0-9]+/) || !url || !(url ==~ /https?:\/\/[a-zA-Z0-9.:\/-]+/)) {
        error('Set PSOFT_SSH_TARGET, PSOFT_SSH_PORT and the environment URLs in Jenkins.')
    }
    return [target: target, port: port, url: url.replaceAll('/$', '')]
}

def call(String environment) {
    def config = settings(environment)
    def deployment = "${env.JOB_NAME}#${env.BUILD_NUMBER}"
    writeFile file: 'artifact/deployment.txt.new', text: "${deployment}\n"
    lock(resource: "psoft-g1-${environment}") {
        timeout(time: 6, unit: 'MINUTES') {
            sshagent(credentials: ['psoft-deploy-ssh']) {
                withEnv(["DEPLOY_ENV=${environment}", "DEPLOY_TARGET=${config.target}", "DEPLOY_PORT=${config.port}"]) {
                    sh '''
                        set -eu
                        scp -o BatchMode=yes -P "$DEPLOY_PORT" artifact/app.jar "$DEPLOY_TARGET:/opt/psoft-g1/$DEPLOY_ENV/app.jar.new"
                        scp -o BatchMode=yes -P "$DEPLOY_PORT" artifact/deployment.txt.new "$DEPLOY_TARGET:/opt/psoft-g1/$DEPLOY_ENV/deployment.txt.new"
                        ssh -T -o BatchMode=yes -p "$DEPLOY_PORT" "$DEPLOY_TARGET" "sh -s -- '$DEPLOY_ENV'" <<'REMOTE'
set -eu
name=$1
cd "/opt/psoft-g1/$name"
test -s app.jar.new
sudo -n /usr/bin/systemctl stop "psoft-g1@$name.service"
mv app.jar.new app.jar
chmod 644 app.jar
sudo -n /usr/bin/systemctl start "psoft-g1@$name.service"
mv deployment.txt.new deployed-build.txt
REMOTE
                    '''
                }
                verify(environment, deployment)
            }
        }
    }
    return deployment
}

def verify(String environment, String deployment) {
    def config = settings(environment)
    def expectedCommit = sh(script: "sed -n 's/^GIT_COMMIT=//p' artifact/release.properties", returnStdout: true).trim()
    if (!(expectedCommit ==~ /[a-fA-F0-9]{40,64}/)) {
        error('The artifact has no valid GIT_COMMIT in release.properties.')
    }
    withEnv(["DEPLOY_ENV=${environment}", "DEPLOY_TARGET=${config.target}",
             "DEPLOY_PORT=${config.port}", "DEPLOY_URL=${config.url}", "DEPLOY_COMMIT=${expectedCommit}"]) {
        def actual = sh(script: '''
            ssh -n -T -o BatchMode=yes -p "$DEPLOY_PORT" "$DEPLOY_TARGET" "cat /opt/psoft-g1/$DEPLOY_ENV/deployed-build.txt"
        ''', returnStdout: true).trim()
        if (actual != deployment) {
            error('The environment no longer runs the selected deployment.')
        }
        withCredentials([usernamePassword(credentialsId: 'psoft-smoke-user',
                usernameVariable: 'SMOKE_USERNAME', passwordVariable: 'SMOKE_PASSWORD')]) {
            sh '''
                set -eu
                ssh -n -T -o BatchMode=yes -p "$DEPLOY_PORT" "$DEPLOY_TARGET" "/usr/bin/systemctl is-active --quiet psoft-g1@$DEPLOY_ENV.service"
                sh deploy/smoke-test.sh "$DEPLOY_URL" "$DEPLOY_ENV" "$DEPLOY_COMMIT"
                ssh -n -T -o BatchMode=yes -p "$DEPLOY_PORT" "$DEPLOY_TARGET" "/usr/bin/systemctl is-active --quiet psoft-g1@$DEPLOY_ENV.service"
            '''
        }
    }
}

return this
