package com.ala.recruitment.reservations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RoomRepository extends JpaRepository<Room, UUID> {

}
