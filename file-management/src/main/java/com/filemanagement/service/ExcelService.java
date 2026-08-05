package com.filemanagement.service;

import java.util.List;
import java.util.Map;

public interface ExcelService {
    List<String> getSheetNames(Long fileId);
}
