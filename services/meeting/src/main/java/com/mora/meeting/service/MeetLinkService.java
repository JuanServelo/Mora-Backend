package com.mora.meeting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetLinkService {
    private final GoogleMeetOAuthService oauth;
    public String createLink() {
        return oauth.createLink(com.mora.meeting.security.AuthContext.tenant());
    }
}
