package com.pickleball.pickleball_backend.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "venue_photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenuePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private String photoUrl;
    private Integer displayOrder = 0;
}
