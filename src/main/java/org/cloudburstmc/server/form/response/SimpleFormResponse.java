package org.cloudburstmc.server.form.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.cloudburstmc.server.form.element.ElementButton;

@RequiredArgsConstructor
@Getter
@ToString
public class SimpleFormResponse {

    private final int clickedId;
    private final ElementButton button;
}
