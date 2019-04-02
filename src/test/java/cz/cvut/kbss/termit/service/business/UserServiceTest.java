/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.event.LoginAttemptsThresholdExceeded;
import cz.cvut.kbss.termit.exception.AuthorizationException;
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
    void existsChecksForUsernameExistenceInRepositoryService() {
        final String username = "user@termit";
        sut.exists(username);
        verify(repositoryServiceMock).exists(username);
    }

    @Test
    void unlockUnlocksUserAccountAndUpdatesItViaRepositoryService() {
        final UserAccount account = Generator.generateUserAccount();
        account.lock();
        sut.unlock(account, "newPassword");
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertFalse(captor.getValue().isLocked());
    }

    @Test
    void unlockSetsNewPasswordOnAccountSpecifiedAsArgument() {
        final UserAccount account = Generator.generateUserAccount();
        account.lock();
        final String newPassword = "newPassword";
        sut.unlock(account, newPassword);
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertEquals(newPassword, captor.getValue().getPassword());
    }

    @Test
    void disableDisablesUserAccountAndUpdatesItViaRepositoryService() {
        final UserAccount account = Generator.generateUserAccount();
        sut.disable(account);
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertFalse(captor.getValue().isEnabled());
    }

    @Test
    void enableEnablesUserAccountAndUpdatesItViaRepositoryService() {
        final UserAccount account = Generator.generateUserAccount();
        account.disable();
        sut.enable(account);
        final ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repositoryServiceMock).update(captor.capture());
        assertTrue(captor.getValue().isEnabled());
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