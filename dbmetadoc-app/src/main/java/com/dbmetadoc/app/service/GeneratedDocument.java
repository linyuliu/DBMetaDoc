package com.dbmetadoc.app.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDocument {

    private String fileName;

    private String contentType;

    private byte[] content;
}
