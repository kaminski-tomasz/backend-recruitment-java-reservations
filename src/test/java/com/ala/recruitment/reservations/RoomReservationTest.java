package com.ala.recruitment.reservations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class RoomReservationTest {

    private final UUID user1 = UUID.randomUUID();
    private final UUID user2 = UUID.randomUUID();
    private final UUID room1 = UUID.randomUUID();
    private final UUID room2 = UUID.randomUUID();
    private final LocalDate firstDay = LocalDate.ofYearDay(2022, 1);
    private final LocalDate fifthDay = LocalDate.ofYearDay(2022, 5);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomReservationService sut;

    @BeforeEach
    public void setup() {
        roomRepository.save(new Room(room1));
        roomRepository.save(new Room(room2));
    }

    @Test
    public void makesSingleReservation() {
        MakeReservationCommand command = new MakeReservationCommand(room1, user1, firstDay, fifthDay);

        sut.makeReservation(command);

        assertThat(sut.isReserved(new ReservationQuery(room1, user1, firstDay))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user1, firstDay.plusDays(1L)))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user1, firstDay.plusDays(2L)))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user1, firstDay.plusDays(3L)))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user1, fifthDay))).isFalse();
        assertThat(sut.reservationsCount(command.customerId)).isEqualTo(1);
    }

    @Test
    public void makesMultipleReservations() {
        sut.makeReservation(new MakeReservationCommand(room1, user1, firstDay, firstDay.plusDays(1)));
        sut.makeReservation(new MakeReservationCommand(room2, user2, firstDay, firstDay.plusDays(1)));

        assertThat(sut.isReserved(new ReservationQuery(room1, user1, firstDay))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user2, firstDay))).isFalse();
        assertThat(sut.isReserved(new ReservationQuery(room2, user2, firstDay))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room2, user1, firstDay))).isFalse();
        assertThat(sut.reservationsCount(Set.of(user1, user2))).isEqualTo(2);
    }

    @Test
    public void doesNotAllowReservingOccupiedRooms() {
        sut.makeReservation(new MakeReservationCommand(room1, user1, firstDay, fifthDay));

        assertThatThrownBy(() -> sut.makeReservation(new MakeReservationCommand(room1, user2, firstDay, fifthDay)))
                .isInstanceOf(RoomNotAvailableException.class);
        assertThatThrownBy(() -> sut.makeReservation(new MakeReservationCommand(room1, user2, firstDay, firstDay.plusDays(1))))
                .isInstanceOf(RoomNotAvailableException.class);
        assertThatThrownBy(() -> sut.makeReservation(new MakeReservationCommand(room1, user2, firstDay.minusDays(1), firstDay.plusDays(1))))
                .isInstanceOf(RoomNotAvailableException.class);
        assertThatThrownBy(() -> sut.makeReservation(new MakeReservationCommand(room1, user2, firstDay.plusDays(1), fifthDay.plusDays(1))))
                .isInstanceOf(RoomNotAvailableException.class);
        assertThatThrownBy(() -> sut.makeReservation(new MakeReservationCommand(room1, user2, firstDay.plusDays(1), fifthDay)))
                .isInstanceOf(RoomNotAvailableException.class);
        assertThat(sut.reservationsCount(user1)).isEqualTo(1);
    }

    @Test
    public void allowsNonCollidingReservations() {
        sut.makeReservation(new MakeReservationCommand(room1, user1, firstDay, fifthDay));
        sut.makeReservation(new MakeReservationCommand(room1, user1, firstDay.minusDays(2), firstDay));
        sut.makeReservation(new MakeReservationCommand(room1, user1, fifthDay, fifthDay.plusDays(2)));

        assertThat(sut.isReserved(new ReservationQuery(room1, user1, firstDay))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user1, firstDay.minusDays(1)))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user1, fifthDay))).isTrue();
        assertThat(sut.isReserved(new ReservationQuery(room1, user1, fifthDay.plusDays(1)))).isTrue();
        assertThat(sut.reservationsCount(user1)).isEqualTo(3);
    }

}
