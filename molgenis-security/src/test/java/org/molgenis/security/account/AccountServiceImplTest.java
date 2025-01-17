package org.molgenis.security.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.data.populate.IdGenerator.Strategy.SECURE_RANDOM;
import static org.molgenis.data.security.auth.UserMetadata.ACTIVATIONCODE;
import static org.molgenis.data.security.auth.UserMetadata.ACTIVE;
import static org.molgenis.data.security.auth.UserMetadata.USER;

import java.net.URISyntaxException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.molgenis.data.DataService;
import org.molgenis.data.Query;
import org.molgenis.data.populate.IdGenerator;
import org.molgenis.data.security.auth.User;
import org.molgenis.data.security.user.UserService;
import org.molgenis.security.settings.AuthenticationSettings;
import org.molgenis.security.user.MolgenisUserException;
import org.molgenis.settings.AppSettings;
import org.molgenis.test.AbstractMockitoTestNGSpringContextTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration
public class AccountServiceImplTest extends AbstractMockitoTestNGSpringContextTests {
  @Autowired private AccountService accountService;

  @Autowired private DataService dataService;

  @Autowired private MailSender mailSender;

  @Mock private User user;

  @Autowired private AppSettings appSettings;

  @Autowired private AuthenticationSettings authenticationSettings;

  @Autowired private IdGenerator idGenerator;

  public AccountServiceImplTest() {
    super(Strictness.WARN);
  }

  @BeforeMethod
  public void setUp() {
    when(appSettings.getTitle()).thenReturn("Molgenis title");
    when(authenticationSettings.getSignUpModeration()).thenReturn(false);

    when(user.getUsername()).thenReturn("jansenj");
    when(user.getFirstName()).thenReturn("Jan");
    when(user.getMiddleNames()).thenReturn("Piet Hein");
    when(user.getLastName()).thenReturn("Jansen");
    when(user.getEmail()).thenReturn("jan.jansen@activation.nl");
    when(user.getPassword()).thenReturn("password");
    when(user.isActive()).thenReturn(true);

    reset(mailSender);
  }

  @Test
  public void activateUser() {
    @SuppressWarnings("unchecked")
    Query<User> q = mock(Query.class);
    when(q.eq(ACTIVE, false)).thenReturn(q);
    when(q.and()).thenReturn(q);
    when(q.eq(ACTIVATIONCODE, "123")).thenReturn(q);
    when(q.findOne()).thenReturn(user);
    when(dataService.query(USER, User.class)).thenReturn(q);

    accountService.activateUser("123");

    ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
    verify(dataService).update(eq(USER), argument.capture());
    verify(user).setActive(true);

    SimpleMailMessage expected = new SimpleMailMessage();
    expected.setTo("jan.jansen@activation.nl");
    expected.setText(
        "Dear Jan Jansen,\n\nYour registration request for Molgenis title was approved.\n"
            + "Your account is now active.\n\n"
            + "Kind regards,\n\n"
            + "Server administrator");
    expected.setSubject("Your registration request for Molgenis title");
    verify(mailSender).send(expected);
  }

  @Test(expectedExceptions = MolgenisUserException.class)
  public void activateUser_invalidActivationCode() {
    @SuppressWarnings("unchecked")
    Query<User> q = mock(Query.class);
    when(q.eq(ACTIVE, false)).thenReturn(q);
    when(q.and()).thenReturn(q);
    when(q.eq(ACTIVATIONCODE, "invalid")).thenReturn(q);
    when(q.findOne()).thenReturn(null);
    when(dataService.query(USER, User.class)).thenReturn(q);

    accountService.activateUser("invalid");
  }

  @Test(expectedExceptions = MolgenisUserException.class)
  public void activateUser_alreadyActivated() {
    @SuppressWarnings("unchecked")
    Query<User> q = mock(Query.class);
    when(q.eq(ACTIVE, false)).thenReturn(q);
    when(q.and()).thenReturn(q);
    when(q.eq(ACTIVATIONCODE, "456")).thenReturn(q);
    when(q.findOne()).thenReturn(null);
    when(dataService.query(USER, User.class)).thenReturn(q);

    accountService.activateUser("456");
  }

  @Test
  public void createUser()
      throws URISyntaxException, UsernameAlreadyExistsException, EmailAlreadyExistsException {

    when(idGenerator.generateId(SECURE_RANDOM)).thenReturn("3541db68-435b-416b-8c2c-cf2edf6ba435");

    accountService.createUser(user, "http://molgenis.org/activate");

    ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
    verify(dataService).add(eq(USER), argument.capture());
    verify(argument.getValue()).setActive(false);

    SimpleMailMessage expected = new SimpleMailMessage();
    expected.setTo("jan.jansen@activation.nl");
    expected.setSubject("User registration for Molgenis title");
    expected.setText(
        "User registration for Molgenis title\n"
            + "User name: jansenj Full name: Jan Jansen\n"
            + "In order to activate the user visit the following URL:\n"
            + "http://molgenis.org/activate/3541db68-435b-416b-8c2c-cf2edf6ba435\n\n");

    verify(mailSender).send(expected);
  }

  @SuppressWarnings("deprecation")
  @Test(
      expectedExceptions = MolgenisUserException.class,
      expectedExceptionsMessageRegExp =
          "An error occurred. Please contact the administrator. You are not signed up!")
  public void createUserMailSendFailed()
      throws UsernameAlreadyExistsException, EmailAlreadyExistsException {

    when(idGenerator.generateId(SECURE_RANDOM)).thenReturn("3541db68-435b-416b-8c2c-cf2edf6ba435");
    doThrow(new MailSendException("mail send failed"))
        .when(mailSender)
        .send(any(SimpleMailMessage.class));
    accountService.createUser(user, "http://molgenis.org/activate");
  }

  @Configuration
  static class Config {
    @Bean
    public AccountService accountService() {
      return new AccountServiceImpl(
          dataService(),
          mailSender(),
          molgenisUserService(),
          appSettings(),
          authenticationSettings(),
          idGenerator());
    }

    @Bean
    public IdGenerator idGenerator() {
      return mock(IdGenerator.class);
    }

    @Bean
    public DataService dataService() {
      return mock(DataService.class);
    }

    @Bean
    public AppSettings appSettings() {
      return mock(AppSettings.class);
    }

    @Bean
    public AuthenticationSettings authenticationSettings() {
      return mock(AuthenticationSettings.class);
    }

    @Bean
    public MailSender mailSender() {
      return mock(MailSender.class);
    }

    @Bean
    public UserService molgenisUserService() {
      return mock(UserService.class);
    }
  }
}
