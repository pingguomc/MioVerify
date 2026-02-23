package org.miowing.mioverify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@SpringBootTest
class MioVerifyApplicationTests {

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
    }

}
