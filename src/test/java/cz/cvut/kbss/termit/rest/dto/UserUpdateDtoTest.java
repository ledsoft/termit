package cz.cvut.kbss.termit.rest.dto;

import cz.cvut.kbss.termit.model.UserAccount;
import org.junit.jupiter.api.Test;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserUpdateDtoTest {

    @Test
    void asUserAccountCopiesAllAttributesIntoNewUserInstance() {
        final UserAccount user = generateAccount();
        final UserUpdateDto dto = new UserUpdateDto();
        dto.setUri(user.getUri());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setTypes(user.getTypes());
        dto.setOriginalPassword("test");

        final UserAccount result = dto.asUserAccount();
        assertEquals(user.getUri(), result.getUri());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(), result.getLastName());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getPassword(), result.getPassword());
        assertEquals(user.getTypes(), result.getTypes());
    }
}