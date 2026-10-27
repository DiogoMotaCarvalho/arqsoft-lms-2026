package pt.psoft.g1.psoftg1.readermanagement.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.nio.file.InvalidPathException;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

@Entity
@Table(name = "READER_DETAILS")
public class ReaderDetails extends EntityWithPhoto {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long pk;

  @Getter @Setter @OneToOne private Reader reader;

  private ReaderNumber readerNumber;

  @Embedded @Getter private BirthDate birthDate;

  @Embedded private PhoneNumber phoneNumber;

  @Setter @Getter @Basic private boolean gdprConsent;

  @Setter @Basic @Getter private boolean marketingConsent;

  @Setter @Basic @Getter private boolean thirdPartySharingConsent;

  @Version @Getter private Long version;

  @Getter @Setter @ManyToMany private List<Genre> interestList;

  protected ReaderDetails() {
    // for ORM only
  }

  public ReaderDetails(
      int readerNumber,
      Reader reader,
      String birthDate,
      String phoneNumber,
      boolean gdpr,
      boolean marketing,
      boolean thirdParty,
      String photoURI,
      List<Genre> interestList) {
    if (reader == null || phoneNumber == null) {
      throw new IllegalArgumentException("Provided argument resolves to null object");
    }

    if (!gdpr) {
      throw new IllegalArgumentException("Readers must agree with the GDPR rules");
    }

    setReader(reader);
    setReaderNumber(new ReaderNumber(readerNumber));
    setPhoneNumber(new PhoneNumber(phoneNumber));
    setBirthDate(new BirthDate(birthDate));
    // By the client specifications, gdpr can only have the value of true. A setter will be created
    // anyways in case we have accept no gdpr consent later on the project
    setGdprConsent(true);

    setPhotoInternal(photoURI);
    setMarketingConsent(marketing);
    setThirdPartySharingConsent(thirdParty);
    setInterestList(interestList);
  }

  private void setPhoneNumber(PhoneNumber number) {
    if (number != null) {
      this.phoneNumber = number;
    }
  }

  private void setReaderNumber(ReaderNumber readerNumber) {
    if (readerNumber != null) {
      this.readerNumber = readerNumber;
    }
  }

  private void setBirthDate(BirthDate date) {
    if (date != null) {
      this.birthDate = date;
    }
  }

  public void applyPatch(
      final long currentVersion,
      final String username,
      final String password,
      final String fullName,
      final String birthDate,
      final String phoneNumber,
      final boolean marketing,
      final boolean thirdParty,
      String photoURI,
      List<Genre> interestList) {
    if (currentVersion != this.version) {
      throw new ConflictException("Provided version does not match latest version of this object");
    }

    if (username != null) {
      this.reader.setUsername(username);
    }

    if (password != null) {
      this.reader.setPassword(password);
    }

    if (fullName != null) {
      this.reader.setName(fullName);
    }

    if (birthDate != null) {
      setBirthDate(new BirthDate(birthDate));
    }

    if (phoneNumber != null) {
      setPhoneNumber(new PhoneNumber(phoneNumber));
    }

    if (marketing != this.marketingConsent) {
      setMarketingConsent(marketing);
    }

    if (thirdParty != this.thirdPartySharingConsent) {
      setThirdPartySharingConsent(thirdParty);
    }

    if (photoURI != null) {
      try {
        setPhotoInternal(photoURI);
      } catch (InvalidPathException expected) {
        // ignore invalid photo paths
      }
    }

    if (interestList != null) {
      this.interestList = interestList;
    }
  }

  public void removePhoto(long desiredVersion) {
    if (desiredVersion != this.version) {
      throw new ConflictException("Provided version does not match latest version of this object");
    }

    setPhotoInternal(null);
  }

  public String getReaderNumber() {
    return this.readerNumber.toString();
  }

  public String getPhoneNumber() {
    return this.phoneNumber.toString();
  }
}
