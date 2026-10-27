package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.impl;

import java.util.List;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;

interface UserRepoCustom {

  List<User> searchUsers(Page page, SearchUsersQuery query);
}
