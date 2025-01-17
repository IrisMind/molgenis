package org.molgenis.security.user;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.molgenis.data.security.auth.User;
import org.molgenis.data.security.user.UserService;
import org.molgenis.security.user.UserAccountServiceImplTest.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@ContextConfiguration(classes = {Config.class})
public class UserAccountServiceImplTest extends AbstractTestNGSpringContextTests {
  private static final String USERNAME_USER = "username";
  private Authentication authentication;
  @Autowired private UserAccountServiceImpl userAccountServiceImpl;
  @Autowired private UserService userService;
  @Autowired private PasswordEncoder passwordEncoder;
  private SecurityContext previousContext;

  @AfterClass
  public void tearDownAfterClass() {
    SecurityContextHolder.setContext(previousContext);
  }

  @BeforeClass
  public void setUpBeforeClass() {
    previousContext = SecurityContextHolder.getContext();
    SecurityContext testContext = SecurityContextHolder.createEmptyContext();
    authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(USERNAME_USER);
    testContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(testContext);
  }

  @Test
  public void getCurrentUser() {
    when(authentication.getPrincipal()).thenReturn(USERNAME_USER);

    User existingUser = mock(User.class);
    when(userService.getUser(USERNAME_USER)).thenReturn(existingUser);
    assertEquals(userAccountServiceImpl.getCurrentUser(), existingUser);
  }

  @Test
  public void updateCurrentUser() {
    User existingUser = mock(User.class);
    when(existingUser.getId()).thenReturn("1");
    when(existingUser.getUsername()).thenReturn(USERNAME_USER);
    when(existingUser.getPassword()).thenReturn("encrypted-password");

    when(userService.getUser(USERNAME_USER)).thenReturn(existingUser);

    User updatedUser = mock(User.class);
    when(updatedUser.getId()).thenReturn("1");
    when(updatedUser.getUsername()).thenReturn("username");
    when(updatedUser.getPassword()).thenReturn("encrypted-password");

    userAccountServiceImpl.updateCurrentUser(updatedUser);
    verify(passwordEncoder, never()).encode("encrypted-password");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void updateCurrentUser_wrongUser() {
    User existingUser = mock(User.class);
    when(existingUser.getId()).thenReturn("1");
    when(existingUser.getPassword()).thenReturn("encrypted-password");

    when(userService.getUser(USERNAME_USER)).thenReturn(existingUser);

    User updatedUser = mock(User.class);
    when(updatedUser.getId()).thenReturn("1");
    when(updatedUser.getUsername()).thenReturn("wrong-username");
    when(updatedUser.getPassword()).thenReturn("encrypted-password");

    userAccountServiceImpl.updateCurrentUser(updatedUser);
  }

  @Test
  public void updateCurrentUser_changePassword() {
    when(passwordEncoder.matches("new-password", "encrypted-password")).thenReturn(true);
    User existingUser = mock(User.class);
    when(existingUser.getId()).thenReturn("1");
    when(existingUser.getPassword()).thenReturn("encrypted-password");
    when(existingUser.getUsername()).thenReturn("username");

    when(userService.getUser(USERNAME_USER)).thenReturn(existingUser);

    User updatedUser = mock(User.class);
    when(updatedUser.getId()).thenReturn("1");
    when(updatedUser.getPassword()).thenReturn("new-password");
    when(updatedUser.getUsername()).thenReturn("username");

    userAccountServiceImpl.updateCurrentUser(updatedUser);
  }

  @Test
  public void validateCurrentUserPassword() {
    User existingUser = mock(User.class);
    when(existingUser.getId()).thenReturn("1");
    when(existingUser.getPassword()).thenReturn("encrypted-password");
    when(existingUser.getUsername()).thenReturn("username");
    when(passwordEncoder.matches("password", "encrypted-password")).thenReturn(true);
    assertTrue(userAccountServiceImpl.validateCurrentUserPassword("password"));
    assertFalse(userAccountServiceImpl.validateCurrentUserPassword("wrong-password"));
  }

  @Configuration
  static class Config {
    @Bean
    public UserAccountServiceImpl userAccountServiceImpl() {
      return new UserAccountServiceImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
      return mock(PasswordEncoder.class);
    }

    @Bean
    public UserService molgenisUserService() {
      return mock(UserService.class);
    }
  }
}
