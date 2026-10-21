package com.mora.meeting.service;

import com.google.api.services.meet.v2.Meet;
import com.google.api.services.meet.v2.model.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MeetLinkService {
    private final ObjectProvider<Meet> clients;
    public String createLink() {
        try {
            Meet client = clients.getIfAvailable();
            return client == null ? null : client.spaces().create(new Space()).execute().getMeetingUri();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A integração Google Meet está indisponível. Verifique as credenciais e os tokens externos.");
        }
    }
}
