
INSERT INTO rescue_centers (code, name, city)
VALUES ('RC-001', 'Centro Caribe', 'Barranquilla');

INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id)
SELECT v.case_code, v.rescue_date::date, v.loc, v.status, c.id
FROM rescue_centers c,
     (VALUES
        ('RC-2026-001', '2026-10-01', 'Playa Puerto Colombia', 'ADMITTED'),
        ('RC-2026-002', '2026-10-05', 'Bocas de Ceniza',       'UNDER_EVALUATION'),
        ('RC-2026-003', '2026-10-10', 'Isla Salamanca',        'IN_REHABILITATION')
     ) AS v(case_code, rescue_date, loc, status)
WHERE c.code = 'RC-001';

INSERT INTO animals (animal_code, common_name, scientific_name, sex, rescue_case_id)
SELECT v.animal_code, v.common_name, v.sci, v.sex, rc.id
FROM rescue_cases rc
JOIN (VALUES
        ('RC-2026-001', 'AN-001', 'Tortuga verde',          'Chelonia mydas',         'FEMALE'),
        ('RC-2026-002', 'AN-002', 'Delfín nariz de botella','Tursiops truncatus',     'MALE'),
        ('RC-2026-003', 'AN-003', 'Pelícano pardo',         'Pelecanus occidentalis', 'FEMALE')
     ) AS v(case_code, animal_code, common_name, sci, sex)
  ON v.case_code = rc.case_code;

INSERT INTO specialists (professional_code, first_name, last_name, email, active)
VALUES ('SP-001', 'Laura', 'Gómez', 'laura@deepblue.com', true);
