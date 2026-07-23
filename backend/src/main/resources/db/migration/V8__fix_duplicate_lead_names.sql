-- Update all existing leads to have unique realistic names and matching emails
DO $$
DECLARE
  rec RECORD;
  idx INTEGER := 1;
  first_names VARCHAR[] := ARRAY[
    'Carlos', 'Mariana', 'Amit', 'Priya', 'Wang', 'Zhang', 'Alejandro', 'Isabella', 'Jean', 'Sophie',
    'Lucas', 'Maria', 'Fatima', 'Youssef', 'Yuki', 'Kenji', 'Olga', 'Dmitry', 'Chloe', 'Pierre',
    'Alessandro', 'Giulia', 'Mateo', 'Sofia', 'Liam', 'Emma', 'William', 'Olivia', 'Nguyen', 'Tran',
    'Gabriel', 'Beatriz', 'Diego', 'Camila', 'Rodrigo', 'Larissa', 'Thiago', 'Fernanda', 'Rafael', 'Amanda',
    'Bruno', 'Juliana', 'Felipe', 'Leticia', 'Gustavo', 'Carla', 'Vinicius', 'Patricia', 'Leonardo', 'Vanessa',
    'Eduardo', 'Natalia', 'Marcelo', 'Rafaela', 'Guilherme', 'Tatiana', 'Caio', 'Daniela', 'Henrique', 'Priscila',
    'Otavio', 'Bianca', 'Renato', 'Monique', 'Fabio', 'Sabrina', 'Murilo', 'Carolina', 'Igor', 'Aline'
  ];
  last_names VARCHAR[] := ARRAY[
    'Gomez', 'Garcia', 'Patel', 'Sharma', 'Li', 'Wei', 'Rodriguez', 'Martinez', 'Dupont', 'Martin',
    'Silva', 'Santos', 'Zahra', 'Mansour', 'Tanaka', 'Sato', 'Smirnova', 'Ivanov', 'Dubois', 'Leroy',
    'Rossi', 'Bianchi', 'Fernandez', 'Romero', 'O Connor', 'Murphy', 'Brown', 'Smith', 'Van An', 'Thi Bich',
    'Ferreira', 'Almeida', 'Oliveira', 'Costa', 'Pereira', 'Carvalho', 'Ribeiro', 'Rodrigues', 'Nascimento', 'Lima',
    'Souza', 'Araujo', 'Mendes', 'Barros', 'Freitas', 'Barbosa', 'Pinto', 'Moreira', 'Cavalcanti', 'Dias'
  ];
  new_fname VARCHAR;
  new_lname VARCHAR;
  full_n VARCHAR;
  new_em VARCHAR;
BEGIN
  FOR rec IN SELECT id FROM leads ORDER BY created_at ASC LOOP
    new_fname := first_names[1 + (idx % array_length(first_names, 1))];
    new_lname := last_names[1 + ((idx * 13) % array_length(last_names, 1))];
    full_n := new_fname || ' ' || new_lname;
    new_em := lower(new_fname) || '.' || lower(replace(new_lname, ' ', '')) || idx || '@example.com';

    UPDATE leads 
    SET full_name = full_n,
        email = new_em
    WHERE id = rec.id;

    idx := idx + 1;
  END LOOP;
END $$;
