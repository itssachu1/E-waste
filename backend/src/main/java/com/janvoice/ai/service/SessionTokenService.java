package com.janvoice.ai.service;

import com.janvoice.ai.entity.User;

public interface SessionTokenService {
    String issue(User user);
    User authenticate(String authorizationHeader);
}
