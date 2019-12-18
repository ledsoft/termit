package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.event.LoginAttemptsThresholdExceeded;
import cz.cvut.kbss.termit.exception.AuthorizationException;
import cz.cvut.kbss.termit.exception.UnsupportedOperationException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.rest.dto.UserUpdateDto;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepositoryService repositoryServiceMock;

    @Mock
    private SecurityUtils securityUtilsMock;

    @InjectMocks
    private UserService sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void findAllLoadsUsersFromRepositoryService() {
        sut.findAll();
        verify(repositoryServiceMock).findAll();
    }

    @Test
    void persistPassesAccountToPersistToRepositoryService() {
        final UserAccount toPersist = Generator.generateUserAccount();
        sut.persist(toPersist);
        verify(repositoryServiceMock).persist(toPersist);
    }

    @Test
    void updateVerifiesOriginalPasswordBeforeUpdatingAccountWithNewPassword() {
        final UserUpdateDto update = new UserUpdateDto();
        update.setUri(Generator.generateUri());
        update.setFirstName("firstName");
        update.setLastName("lastName");
        update.setUsername("username");
        update.setPassword("password");
        update.setOriginalPassword("originalPassword");
        when(securityUtilsMock.getCurrentUser()).thenReturn(update.asUserAccount());
        sut.updateCurrent(update);
        final InOrder inOrder = Mockito.inOrder(securityUtilsMock, repositoryServiceMock);
        inOrder.verify(securityUtilsMock).verifyCurrentUserPassword(update.getOriginalPassword());
        inOrder.verify(repositoryServiceMock).update(update.asUserAccount());
    }

    @Test
    void updateDoesNotVerifyOriginalPasswordWhenAccountDoesNotUpdatePassword() {
        final UserUpdateDto update = new UserUpdateDto();
        update.setUri(Generator.generateUri());
        update.setFirstName("firstName");
        update.setLastName("lastName");
        update.setUsername("username");
        when(securityUtilsMock.getCurrentUser()).thenReturn(update.asUserAccount());
        sut.updateCurrent(update);
        verify(repositoryServiceMock).update(update.asUserAccount());
        verify(securityUtilsMock, never()).verifyCurrentUserPassword(any());
    }

    @Test
    void updateThrowsAuthorizationExceptionWhenAttemptingToUpdateDifferentUserThatCurrent() {
        final UserUpdateDto update = new UserUpdateDto();
        update.setUri(Generator.generateUri());
        update.setFirstName("firstName");
        update.setLastName("lastName");
        update.setUsername("username");
        final UserAccount ua = Generator.generateUserAccount();
        when(securityUtilsMock.getCurrentUser()).thenReturn(ua);
        assertThrows(AuthorizationException.class, () -> sut.updateCurrent(update));
    }

    @Test
    void updateThrowsValidationExceptionWhenAttemptingToUpdateUsernameOfAccount() {
        final UserAccount ua = Generator.generateUserAccount();
        final UserUpdateDto update = new UserUpdateDto();

        update.setUri(ua.getUri());
        update.setUsername("username");

        when(securityUtilsMock.getCurrentUser()).thenReturn(ua);
        assertThrows(ValidationException.class, () -> sut.updateCurrent(update));
    }

    @Test
    void existsChecksForUsernameExistenceInRepositoryService() {
        final String username = "user@termit";
        sut.exists(username);
        verify(repositoryServiceMock).exists(username);
    }

    @Test
    void unlockUnlocksUserAccountAndUpdatesItViaRepositoryService() {
        when(securityUtilsMock.getCurrentUser()).thenReturn(Generator.generateUserAccount());
        final UserAccount account = Generator.generateUserAccount();
        account.lock();
        sut.unlock(account, "newPassword");
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertFalse(captor.getValue().isLocked());
    }

    @Test
    void unlockSetsNewPasswordOnAccountSpecifiedAsArgument() {
        when(securityUtilsMock.getCurrentUser()).thenReturn(Generator.generateUserAccount());
        final UserAccount account = Generator.generateUserAccount();
        account.lock();
        final String newPassword = "newPassword";
        sut.unlock(account, newPassword);
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertEquals(newPassword, captor.getValue().getPassword());
    }

    @Test
    void unlockThrowsUnsupportedOperationExceptionWhenAttemptingToUnlockOwnAccount() {
        final UserAccount account = Generator.generateUserAccount();
        account.lock();
        when(securityUtilsMock.getCurrentUser()).thenReturn(account);
        assertThrows(UnsupportedOperationException.class, () -> sut.unlock(account, "newPassword"));
        verify(repositoryServiceMock, never()).update(any());
    }

    @Test
    void disableDisablesUserAccountAndUpdatesItViaRepositoryService() {
        when(securityUtilsMock.getCurrentUser()).thenReturn(Generator.generateUserAccount());
        final UserAccount account = Generator.generateUserAccount();
        sut.disable(account);
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertFalse(captor.getValue().isEnabled());
    }

    @Test
    void disableThrowsUnsupportedOperationExceptionWhenAttemptingToDisableOwnAccount() {
        final UserAccount account = Generator.generateUserAccount();
        when(securityUtilsMock.getCurrentUser()).thenReturn(account);
        assertThrows(UnsupportedOperationException.class, () -> sut.disable(account));
        verify(repositoryServiceMock, never()).update(any());
    }

    @Test
    void enableEnablesUserAccountAndUpdatesItViaRepositoryService() {
        when(securityUtilsMock.getCurrentUser()).thenReturn(Generator.generateUserAccount());
        final UserAccount account = Generator.generateUserAccount();
        account.disable();
        sut.enable(account);
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertTrue(captor.getValue().isEnabled());
    }

    @Test
    void enableThrowsUnsupportedOperationExceptionWhenAttemptingToEnableOwnAccount() {
        final UserAccount account = Generator.generateUserAccount();
        account.disable();
        when(securityUtilsMock.getCurrentUser()).thenReturn(account);
        assertThrows(UnsupportedOperationException.class, () -> sut.enable(account));
        verify(repositoryServiceMock, never()).update(any());
    }

    @Test
    void onLoginAttemptsThresholdExceededLocksUserAccountAndUpdatesItViaRepositoryService() {
        final UserAccount account = Generator.generateUserAccount();
        sut.onLoginAttemptsThresholdExceeded(new LoginAttemptsThresholdExceeded(account));
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertTrue(captor.getValue().isLocked());
    }

    @Test
    void getCurrentRetrievesCurrentlyLoggedInUserAccount() {
        final UserAccount account = Generator.generateUserAccount();
        when(securityUtilsMock.getCurrentUser()).thenReturn(account);
        final UserAccount result = sut.getCurrent();
        assertEquals(account, result);
    }

    @Test
    void getCurrentReturnsCurrentUserAccountWithoutPassword() {
        final UserAccount account = Generator.generateUserAccount();
        account.setPassword("12345");
        when(securityUtilsMock.getCurrentUser()).thenReturn(account);
        final UserAccount result = sut.getCurrent();
        assertNull(result.getPassword());
    }
}
