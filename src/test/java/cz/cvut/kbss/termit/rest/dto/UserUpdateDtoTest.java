package cz.cvut.kbss.termit.rest.dto;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserUpdateDtoTest {

    @Test
    void toUserCopiesAllAttributesIntoNewUserInstance() {
        final User user = Generator.generateUserWithId();
        final UserUpdateDto dto = new UserUpdateDto();
        dto.setUri(user.getUri());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setTypes(user.getTypes());
        dto.setOriginalPassword("test");

        final User result = dto.toUser();
        assertEquals(user.getUri(), result.getUri());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(),  result.getLastName());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getPassword(), result.getPassword());
        assertEquals(user.getTypes(), result.getTypes());
    }

}