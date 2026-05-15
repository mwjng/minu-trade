package com.minupay.trade.audit.infrastructure;

import com.minupay.trade.audit.domain.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
}
