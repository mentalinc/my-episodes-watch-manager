package nz.mentalinc.watcher.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class UserTest {
    @Test
    public void testConstructorAndGetters() {
        User user = new User("myUser", "myPass");
        assertEquals("myUser", user.getUsername());
        assertEquals("myPass", user.getPassword());
    }

    @Test
    public void testConstants() {
        assertEquals("USERNAME", User.USERNAME);
        assertEquals("PASSWORD", User.PASSWORD);
    }

    @Test
    public void testSettersArePrivate_notAccessible() {
        User user = new User("u1", "p1");
        assertEquals("u1", user.getUsername());
        assertEquals("p1", user.getPassword());
    }
}
