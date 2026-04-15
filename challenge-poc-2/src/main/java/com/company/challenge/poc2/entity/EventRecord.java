package com.company.challenge.poc2.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data             
@Builder          
@NoArgsConstructor
@AllArgsConstructor
@Table("events")    
public class EventRecord {

    @Id
    private Long id;

    private String data;
}