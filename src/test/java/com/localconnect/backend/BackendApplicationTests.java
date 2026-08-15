package com.localconnect.backend;

import com.localconnect.backend.repository.BookingRepository;
import com.localconnect.backend.repository.ChatMessageRepository;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BackendApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ServiceListingRepository serviceListingRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private ChatMessageRepository chatMessageRepository;

    @Test
    void contextLoads() {
    }

}
