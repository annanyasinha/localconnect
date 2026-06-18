
package com.localconnect.backend.repository;

import com.localconnect.backend.entity.Booking;
import com.localconnect.backend.entity.ServiceListing;
import com.localconnect.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    List<Booking> findByServiceListing(ServiceListing serviceListing);
}