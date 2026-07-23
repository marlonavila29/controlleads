CREATE TABLE communication_logs (
    id UUID PRIMARY KEY,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    sent_by UUID NOT NULL REFERENCES users(id),
    channel VARCHAR(50) NOT NULL, -- EMAIL, WHATSAPP
    recipient_address VARCHAR(255) NOT NULL, -- Email address or phone number
    subject VARCHAR(255),
    body TEXT NOT NULL,
    status VARCHAR(50) NOT NULL, -- SENT, FAILED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_comm_logs_lead ON communication_logs(lead_id);
CREATE INDEX idx_comm_logs_created ON communication_logs(created_at);
