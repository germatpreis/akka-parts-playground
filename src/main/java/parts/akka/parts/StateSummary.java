package parts.akka.parts;

import java.time.LocalDateTime;

record StateSummary(LocalDateTime createdAt, int referencedPartCount) {}
