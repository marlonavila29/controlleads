-- Clean existing mock data and tokens
DELETE FROM refresh_tokens;
DELETE FROM password_reset_tokens;
DELETE FROM activities;
DELETE FROM lead_status_events;
DELETE FROM lead_assignment_events;
DELETE FROM notifications;
DELETE FROM leads;

-- Safely delete existing users with conflicting emails to ensure our custom UUIDs are set
DELETE FROM users WHERE email IN (
  'admin@controlleads.local', 
  'member@controlleads.local', 
  'john@controlleads.local', 
  'maria@controlleads.local', 
  'chen@controlleads.local', 
  'elena@controlleads.local'
);

-- Seed default Users with correct BCrypt hashes for 'admin123' and 'member123'
INSERT INTO users (id, name, email, password_hash, role, active)
VALUES 
  ('11111111-1111-1111-1111-111111111111', 'Administrator', 'admin@controlleads.local', '$2a$10$Ge9y1wG/ymP106NKynldEeaSQB6oNW1b5pmDbj2YwPEU5L.D7hyO.', 'ADMINISTRATOR', true),
  ('22222222-2222-2222-2222-222222222222', 'Sarah Counselor', 'member@controlleads.local', '$2a$10$dEJFsDQmhEY6sjPMw8YNCOSc//el6MyNLgc36cVt5K9xvXy1BguOy', 'MARKETING_TEAM', true),
  ('33333333-3333-3333-3333-333333333333', 'John Doe', 'john@controlleads.local', '$2a$10$dEJFsDQmhEY6sjPMw8YNCOSc//el6MyNLgc36cVt5K9xvXy1BguOy', 'MARKETING_TEAM', true),
  ('44444444-4444-4444-4444-444444444444', 'Maria Silva', 'maria@controlleads.local', '$2a$10$dEJFsDQmhEY6sjPMw8YNCOSc//el6MyNLgc36cVt5K9xvXy1BguOy', 'MARKETING_TEAM', true),
  ('55555555-5555-5555-5555-555555555555', 'Chen Wei', 'chen@controlleads.local', '$2a$10$dEJFsDQmhEY6sjPMw8YNCOSc//el6MyNLgc36cVt5K9xvXy1BguOy', 'MARKETING_TEAM', true),
  ('66666666-6666-6666-6666-666666666666', 'Elena Rostova', 'elena@controlleads.local', '$2a$10$dEJFsDQmhEY6sjPMw8YNCOSc//el6MyNLgc36cVt5K9xvXy1BguOy', 'MARKETING_TEAM', true);

-- Seed default Courses
INSERT INTO courses (id, name, active)
VALUES 
  ('c1111111-1111-1111-1111-111111111111', 'Computer Science', true),
  ('c2222222-2222-2222-2222-222222222222', 'Business Administration', true),
  ('c3333333-3333-3333-3333-333333333333', 'English Language Program', true),
  ('c4444444-4444-4444-4444-444444444444', 'Data Science', true),
  ('c5555555-5555-5555-5555-555555555555', 'MBA', true)
ON CONFLICT (name) DO UPDATE SET active = true;

-- Seed default Campaigns if not exists
INSERT INTO campaigns (id, name, active)
VALUES
  ('a1111111-1111-1111-1111-111111111111', 'fall_intake_2026', true),
  ('a2222222-2222-2222-2222-222222222222', 'spring_intake_2027', true),
  ('a3333333-3333-3333-3333-333333333333', 'summer_camp_2026', true)
ON CONFLICT (name) DO UPDATE SET active = true;

-- Generate realistic mock leads via anonymous PL/pgSQL block
DO $$
DECLARE
  i INTEGER;
  lead_id UUID;
  counselor_id UUID;
  course_id UUID;
  channel_id UUID;
  stall_reason_id UUID;
  lead_name VARCHAR;
  c_code VARCHAR;
  c_email VARCHAR;
  c_phone VARCHAR;
  c_status VARCHAR;
  created_time TIMESTAMPTZ;
  channel_list UUID[];
  stall_list UUID[];
  courses_list UUID[] := ARRAY[
    'c1111111-1111-1111-1111-111111111111'::uuid,
    'c2222222-2222-2222-2222-222222222222'::uuid,
    'c3333333-3333-3333-3333-333333333333'::uuid,
    'c4444444-4444-4444-4444-444444444444'::uuid,
    'c5555555-5555-5555-5555-555555555555'::uuid
  ];
  counselor_list UUID[] := ARRAY[
    '22222222-2222-2222-2222-222222222222'::uuid, -- Sarah
    '33333333-3333-3333-3333-333333333333'::uuid, -- John
    '44444444-4444-4444-4444-444444444444'::uuid, -- Maria
    '55555555-5555-5555-5555-555555555555'::uuid, -- Chen
    '66666666-6666-6666-6666-666666666666'::uuid  -- Elena
  ];
  names_arr VARCHAR[] := ARRAY[
    'Carlos Gomez', 'Mariana Garcia', 'Amit Patel', 'Priya Sharma', 'Wang Li', 
    'Zhang Wei', 'Alejandro Rodriguez', 'Isabella Martinez', 'Jean Dupont', 'Sophie Martin', 
    'Lucas Silva', 'Maria Santos', 'Fatima Zahra', 'Youssef Mansour', 'Yuki Tanaka', 
    'Kenji Sato', 'Olga Smirnova', 'Dmitry Ivanov', 'Chloe Dubois', 'Pierre Leroy', 
    'Alessandro Rossi', 'Giulia Bianchi', 'Mateo Fernandez', 'Sofia Romero', 'Liam O Connor', 
    'Emma Murphy', 'William Brown', 'Olivia Smith', 'Nguyen Van An', 'Tran Thi Bich'
  ];
  countries_arr VARCHAR[] := ARRAY[
    'MX', 'MX', 'IN', 'IN', 'CN', 
    'CN', 'ES', 'ES', 'FR', 'FR', 
    'BR', 'BR', 'MA', 'EG', 'JP', 
    'JP', 'RU', 'RU', 'FR', 'FR', 
    'IT', 'IT', 'AR', 'AR', 'IE', 
    'IE', 'GB', 'CA', 'VN', 'VN'
  ];
  campaign_list VARCHAR[] := ARRAY['fall_intake_2026', 'spring_intake_2027', 'summer_camp_2026'];
  source_list VARCHAR[] := ARRAY['google', 'facebook', 'instagram', 'referral', 'organic'];
  medium_list VARCHAR[] := ARRAY['cpc', 'paid_social', 'email', 'referral', 'organic'];
BEGIN
  -- Gather dynamic ids from database seed catalogs
  SELECT array_agg(id) INTO channel_list FROM channels;
  SELECT array_agg(id) INTO stall_list FROM stall_reasons;

  FOR i IN 1..130 LOOP
    lead_id := gen_random_uuid();
    
    -- Pick fields
    lead_name := names_arr[1 + (i % array_length(names_arr, 1))];

    c_code := countries_arr[1 + (i % array_length(countries_arr, 1))];
    counselor_id := counselor_list[1 + (i % array_length(counselor_list, 1))];
    course_id := courses_list[1 + (i % array_length(courses_list, 1))];
    channel_id := channel_list[1 + (i % array_length(channel_list, 1))];
    
    -- Pick status logic (students, applications, stalled, leads)
    IF i % 6 = 0 THEN
      c_status := 'STUDENT';
    ELSIF i % 6 = 1 THEN
      c_status := 'APPLICATION';
    ELSIF i % 6 = 2 THEN
      c_status := 'HOT_LEAD';
    ELSIF i % 6 = 3 THEN
      c_status := 'LEAD';
    ELSIF i % 6 = 4 THEN
      c_status := 'STALLED';
      stall_reason_id := stall_list[1 + (i % array_length(stall_list, 1))];
    ELSE
      c_status := 'STUDENT';
    END IF;

    -- Spread leads over the last 45 days
    created_time := now() - (i * interval '8 hours');
    c_email := lower(replace(lead_name, ' ', '.')) || i || '@example.com';
    c_phone := '+55 ' || (900000000 + i * 7321);

    -- Insert Lead
    INSERT INTO leads (
      id, full_name, country_code, email, phone, course_id, channel_id, 
      utm_source, utm_medium, utm_campaign, status, stalled_from_status, 
      stall_reason_id, assigned_to, created_by, created_at, updated_at
    ) VALUES (
      lead_id, lead_name, c_code, c_email, c_phone, course_id, channel_id,
      source_list[1 + (i % 5)], medium_list[1 + (i % 5)], campaign_list[1 + (i % 3)],
      c_status, 
      CASE WHEN c_status = 'STALLED' THEN 'HOT_LEAD'::varchar ELSE NULL END,
      CASE WHEN c_status = 'STALLED' THEN stall_reason_id ELSE NULL END,
      counselor_id, '11111111-1111-1111-1111-111111111111'::uuid, 
      created_time, created_time
    );

    -- Insert status audit history events
    -- Stage 1: LEAD
    INSERT INTO lead_status_events (id, lead_id, from_status, to_status, note, changed_by, changed_at)
    VALUES (gen_random_uuid(), lead_id, NULL, 'LEAD', 'Lead created', counselor_id, created_time);

    -- Stage 2: HOT_LEAD
    IF c_status IN ('HOT_LEAD', 'APPLICATION', 'STUDENT', 'STALLED') THEN
      INSERT INTO lead_status_events (id, lead_id, from_status, to_status, note, changed_by, changed_at)
      VALUES (gen_random_uuid(), lead_id, 'LEAD', 'HOT_LEAD', 'Contacted, highly interested', counselor_id, created_time + interval '1 hour');
    END IF;

    -- Stage 3: APPLICATION
    IF c_status IN ('APPLICATION', 'STUDENT') THEN
      INSERT INTO lead_status_events (id, lead_id, from_status, to_status, note, changed_by, changed_at)
      VALUES (gen_random_uuid(), lead_id, 'HOT_LEAD', 'APPLICATION', 'Application form submitted', counselor_id, created_time + interval '2 days');
    END IF;

    -- Stage 4: STUDENT
    IF c_status = 'STUDENT' THEN
      INSERT INTO lead_status_events (id, lead_id, from_status, to_status, note, changed_by, changed_at)
      VALUES (gen_random_uuid(), lead_id, 'APPLICATION', 'STUDENT', 'Enrolled and deposit paid', counselor_id, created_time + interval '5 days');
    END IF;

    -- Stage Stalled
    IF c_status = 'STALLED' THEN
      INSERT INTO lead_status_events (id, lead_id, from_status, to_status, stall_reason_id, note, changed_by, changed_at)
      VALUES (gen_random_uuid(), lead_id, 'HOT_LEAD', 'STALLED', stall_reason_id, 'Paused communication', counselor_id, created_time + interval '1 day');
    END IF;

    -- Insert random timeline activities
    INSERT INTO activities (id, lead_id, type, content, created_by, created_at, updated_at)
    VALUES (
      gen_random_uuid(), lead_id, 'NOTE', 'Added mock profile notes during migration.', 
      counselor_id, created_time + interval '30 minutes', created_time + interval '30 minutes'
    );

    IF c_status IN ('HOT_LEAD', 'APPLICATION', 'STUDENT') THEN
      INSERT INTO activities (id, lead_id, type, content, created_by, created_at, updated_at)
      VALUES (
        gen_random_uuid(), lead_id, 'CALL', 'Spoke to student. Discussion on curriculum and visa details.', 
        counselor_id, created_time + interval '2 hours', created_time + interval '2 hours'
      );
    END IF;

  END LOOP;
END $$;
