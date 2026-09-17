package com.localconnect.backend;

import com.localconnect.backend.config.BookingTools;
import com.localconnect.backend.repository.BookingRepository;
import com.localconnect.backend.repository.ChatMessageRepository;
import com.localconnect.backend.repository.ServiceListingRepository;
import com.localconnect.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",

        "JWT_SECRET=localConnectTestSecretKey12345678901234567890",
        "HF_TOKEN=test-token",
        "HF_BASE_URL=https://example.invalid/v1"
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

    // The AI model and BookingTools require beans that cannot be auto-configured
    // in tests (no real API key). Mock them so the context can load.
    @MockBean
    private ChatModel chatModel;

    @MockBean
    private BookingTools bookingTools;

    @Test
    void contextLoads() {
    }

}
