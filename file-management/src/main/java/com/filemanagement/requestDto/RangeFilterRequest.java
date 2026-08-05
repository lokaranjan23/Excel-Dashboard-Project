package com.filemanagement.requestDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RangeFilterRequest {

    private String column;

    private Object minValue;

    private Object maxValue;

}
