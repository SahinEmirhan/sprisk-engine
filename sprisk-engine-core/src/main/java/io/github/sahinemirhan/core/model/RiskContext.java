package io.github.sahinemirhan.core.model;

import java.time.LocalDateTime;
import java.util.Map;

public record RiskContext (
  String action,
  String userId,
  String ip,
  LocalDateTime timeStamp,
  Map<String , Object> attributes
){}
