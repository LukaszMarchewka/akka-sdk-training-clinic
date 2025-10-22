package com.clinic.application;

import akka.javasdk.testkit.TestKitSupport;
import com.clinic.domain.Appointment;
import com.clinic.domain.Schedule;
import com.clinic.domain.ScheduleAppointmentState;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.clinic.application.DateUtils.dateTime;
import static com.clinic.application.DateUtils.time;
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

        var workflowState = componentClient.forWorkflow("1").method(ScheduleAppointmentWorkflow::getState).invoke();

        assertEquals(ScheduleAppointmentState.Status.AppointmentScheduled, workflowState.status());
    }

    @Test
    public void scheduleTwice() {
    }

    @Test
    public void scheduleOverlapping() {
    }

    @Test
    public void scheduleDoesntExist() {}


}
