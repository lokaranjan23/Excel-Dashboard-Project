package com.filemanagement.requestDto;

import com.filemanagement.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class QueryRequest {


    @NotNull
    @Min(value = 0, message = "Page number cannot be negative")
    private Integer page = 0;

    @NotNull
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 50;


    private String sortColumn;
    private SortDirection sortDirection;

    private List<FilterRequest> filters= new ArrayList<>();

    private List<RangeFilterRequest> rangeFilters = new ArrayList<>();
}