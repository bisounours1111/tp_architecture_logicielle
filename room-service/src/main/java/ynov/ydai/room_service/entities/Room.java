package ynov.ydai.room_service.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String city;
    private Integer capacity;
    
    @Enumerated(EnumType.STRING)
    private RoomType type; // OPEN_SPACE, MEETING_ROOM, PRIVATE_OFFICE
    
    private BigDecimal hourlyRate;
    private boolean available;

    public Room() {}

    public Room(Long id, String name, String city, Integer capacity, RoomType type, BigDecimal hourlyRate, boolean available) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.capacity = capacity;
        this.type = type;
        this.hourlyRate = hourlyRate;
        this.available = available;
    }

    public static RoomBuilder builder() {
        return new RoomBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public static class RoomBuilder {
        private Long id;
        private String name;
        private String city;
        private Integer capacity;
        private RoomType type;
        private BigDecimal hourlyRate;
        private boolean available;

        RoomBuilder() {}

        public RoomBuilder id(Long id) { this.id = id; return this; }
        public RoomBuilder name(String name) { this.name = name; return this; }
        public RoomBuilder city(String city) { this.city = city; return this; }
        public RoomBuilder capacity(Integer capacity) { this.capacity = capacity; return this; }
        public RoomBuilder type(RoomType type) { this.type = type; return this; }
        public RoomBuilder hourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; return this; }
        public RoomBuilder available(boolean available) { this.available = available; return this; }

        public Room build() {
            return new Room(id, name, city, capacity, type, hourlyRate, available);
        }
    }
}
