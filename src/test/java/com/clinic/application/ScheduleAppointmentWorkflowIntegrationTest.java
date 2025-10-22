package com.clinic.application;

import akka.javasdk.testkit.TestKitSupport;
import com.clinic.domain.Appointment;
import com.clinic.domain.Schedule;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScheduleAppointmentWorkflowIntegrationTest extends TestKitSupport {

    @Test
    public void scheduleAppointment() {
        componentClient
                .forKeyValueEntity("house:2031-10-20")
                .method(ScheduleEntity::createSchedule)
                        .invoke(new Schedule.WorkingHours(time("10:00"), time("16:00")));

        componentClient
                .forWorkflow("1")
                .method(ScheduleAppointmentWorkflow::schedule)
                .invoke(new ScheduleAppointmentWorkflow.ScheduleAppointmentCommand(dateTime("2031-10-20T11:00:00"), "house", "2", "issue"));

        Awaitility.await()
                .atMost(10, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    System.out.println("Waiting for appointment to be created");
                    Optional<Appointment> appointment = componentClient
                            .forEventSourcedEntity("1")
                            .method(AppointmentEntity::getAppointment)
                            .invoke();
                    assertTrue(appointment.isPresent());
                    assertEquals(Appointment.Status.SCHEDULED, appointment.get().status());
                });

        var updatedSchedule = componentClient.forKeyValueEntity("house:2031-10-20")
                .method(ScheduleEntity::getSchedule)
                .invoke();
        assertEquals(1, updatedSchedule.get().timeSlots().size());
    }

    @Test
    public void scheduleTwice() {
    }

    @Test
    public void scheduleOverlapping() {
    }

    @Test
    public void scheduleDoesntExist() {}

    private LocalDate date(String str) {
        return LocalDate.parse(str);
    }

    private LocalTime time(String str) {
        return LocalTime.parse(str);
    }

    private LocalDateTime dateTime(String str) {
        return LocalDateTime.parse(str);
    }
}
