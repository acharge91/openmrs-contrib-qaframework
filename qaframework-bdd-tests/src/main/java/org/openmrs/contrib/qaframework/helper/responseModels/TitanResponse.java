package org.openmrs.contrib.qaframework.helper.responseModels;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TitanResponse {

    public Float[] embedding;
    public int inputTextTokenCount;
}
