package com.example.novisapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignTeamRequest {
    private List<Long> userIds;
    private String notes;
}