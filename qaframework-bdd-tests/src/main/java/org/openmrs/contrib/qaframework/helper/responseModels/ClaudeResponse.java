package org.openmrs.contrib.qaframework.helper.responseModels;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaudeResponse {

    private String completion;
    private String stop_reason;

}
