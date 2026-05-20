package com.campussync.backend.Service;


import com.campussync.backend.Dto.DashboardResponse;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public DashboardService(UserRepository userRepository,
                            EventRepository eventRepository,
                            RegistrationRepository registrationRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }



    public DashboardResponse getDashboard() {
        DashboardResponse response = new DashboardResponse();
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        long totalRegistrations = registrationRepository.count();
        long totalViews = eventRepository.sumAllViews();

        response.setTotalUsers(totalUsers);
        response.setTotalEvents(totalEvents);
        response.setTotalRegistrations(totalRegistrations);
        response.setPaidEvents(eventRepository.countByPaidTrue());
        response.setPublishedEvents(eventRepository.countByStatus(EventStatus.PUBLISHED));
        response.setCancelledEvents(eventRepository.countByStatus(EventStatus.CANCELLED));
        response.setTotalEventViews(totalViews);
        response.setAverageRegistrationsPerEvent(totalEvents == 0 ? 0.0 : (double) totalRegistrations / totalEvents);
        response.setEventEngagementRate(totalViews == 0 ? 0.0 : (totalRegistrations * 100.0) / totalViews);
        return response;
    }
}
