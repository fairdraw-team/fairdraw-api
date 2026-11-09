package com.fairdraw.api.entry;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.entry.EntryDtos.*;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class EntryService {
    public MyEntryResponse create(Jwt jwt, UUID eventId, CreateEntryRequest req) { throw ApiException.notImplemented(); }
    public MyEntryResponse getMine(Jwt jwt, UUID eventId) { throw ApiException.notImplemented(); }
    public void cancelMine(Jwt jwt, UUID eventId) { throw ApiException.notImplemented(); }
    public EntryPage listForOrganizer(Jwt jwt, UUID eventId, int page, int size) { throw ApiException.notImplemented(); }
}