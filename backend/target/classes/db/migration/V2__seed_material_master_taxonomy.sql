-- Phase 3: legitimate static material taxonomy only.
-- No prices, recycler records, transactions, or AI predictions are inserted here.

ALTER TABLE material_master ADD CONSTRAINT uk_material_definition
    UNIQUE (category, subcategory, common_name);

CREATE INDEX idx_material_category ON material_master(category);
CREATE INDEX idx_material_subcategory ON material_master(subcategory);
CREATE INDEX idx_material_common_name ON material_master(common_name);
CREATE INDEX idx_material_active ON material_master(active);

INSERT INTO material_master
    (category, subcategory, device_type, common_name, description, recoverable_materials,
     typical_unit, requires_special_handling, battery_related, crt_related, active)
VALUES
    ('IT & Telecom', 'Computing Devices', 'Mobile Phone', 'Mobile Phone', 'Small mobile communication device.', 'PCB; battery; display; copper; aluminium; plastic housing', 'PIECE', TRUE, TRUE, FALSE, TRUE),
    ('IT & Telecom', 'Computing Devices', 'Smartphone', 'Smartphone', 'Mobile phone with a touchscreen operating system.', 'PCB; battery; display; copper; aluminium; plastic housing', 'PIECE', TRUE, TRUE, FALSE, TRUE),
    ('IT & Telecom', 'Computing Devices', 'Laptop', 'Laptop', 'Portable personal computer.', 'PCB; battery; display; copper; aluminium; plastic housing; storage; RAM', 'PIECE', TRUE, TRUE, FALSE, TRUE),
    ('IT & Telecom', 'Computing Devices', 'Desktop', 'Desktop Computer', 'Desktop personal computer and enclosure.', 'PCB; power supply; copper; aluminium; ferrous metal; plastic housing; storage; RAM', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('IT & Telecom', 'Display Devices', 'Monitor', 'Computer Monitor', 'Flat-panel computer display.', 'display; PCB; copper; aluminium; plastic housing', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('IT & Telecom', 'Input Devices', 'Keyboard', 'Keyboard', 'Computer input device.', 'PCB; electronic components; plastic housing; copper', 'PIECE', FALSE, FALSE, FALSE, TRUE),
    ('IT & Telecom', 'Input Devices', 'Mouse', 'Computer Mouse', 'Computer pointing device.', 'PCB; electronic components; plastic housing; copper', 'PIECE', FALSE, FALSE, FALSE, TRUE),
    ('IT & Telecom', 'Network Devices', 'Router', 'Router', 'Network connectivity device.', 'PCB; electronic components; copper; plastic housing', 'PIECE', FALSE, FALSE, FALSE, TRUE),
    ('IT & Telecom', 'Printing Devices', 'Printer', 'Printer', 'Desktop printing equipment.', 'PCB; electronic components; copper; aluminium; ferrous metal; plastic housing', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('IT & Telecom', 'Printing Devices', 'Scanner', 'Scanner', 'Document scanning equipment.', 'PCB; electronic components; copper; aluminium; plastic housing', 'PIECE', FALSE, FALSE, FALSE, TRUE),

    ('Consumer Electronics', 'Display Devices', 'Television', 'Television', 'Consumer television display equipment.', 'display; PCB; copper; aluminium; ferrous metal; plastic housing', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('Consumer Electronics', 'Home Electronics', 'Set-top Box', 'Set-top Box', 'Digital television receiver.', 'PCB; electronic components; copper; plastic housing', 'PIECE', FALSE, FALSE, FALSE, TRUE),
    ('Consumer Electronics', 'Audio Equipment', 'Audio System', 'Audio System', 'Consumer audio playback equipment.', 'PCB; electronic components; copper; aluminium; plastic housing', 'PIECE', FALSE, FALSE, FALSE, TRUE),
    ('Consumer Electronics', 'Audio Equipment', 'Speaker', 'Speaker', 'Audio speaker equipment.', 'copper; aluminium; ferrous metal; plastic housing; electronic components', 'PIECE', FALSE, FALSE, FALSE, TRUE),
    ('Consumer Electronics', 'Imaging Devices', 'Camera', 'Camera', 'Digital or electronic camera equipment.', 'PCB; display; battery; copper; plastic housing', 'PIECE', TRUE, TRUE, FALSE, TRUE),
    ('Consumer Electronics', 'Gaming Devices', 'Gaming Device', 'Gaming Device', 'Electronic gaming console or handheld device.', 'PCB; battery; display; copper; plastic housing', 'PIECE', TRUE, TRUE, FALSE, TRUE),

    ('Electrical Equipment', 'Appliances', 'Fan', 'Electric Fan', 'Powered ventilation appliance.', 'copper; aluminium; ferrous metal; plastic housing; motor', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('Electrical Equipment', 'Motors', 'Motor', 'Electric Motor', 'Standalone electric motor equipment.', 'copper; aluminium; ferrous metal', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('Electrical Equipment', 'Pumps', 'Pump', 'Electric Pump', 'Powered pump equipment.', 'copper; aluminium; ferrous metal; motor; plastic housing', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('Electrical Equipment', 'Appliances', 'Mixer', 'Mixer Grinder', 'Powered kitchen mixer appliance.', 'copper; aluminium; ferrous metal; motor; plastic housing; PCB', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('Electrical Equipment', 'Power Equipment', 'Power Supply', 'Power Supply Unit', 'AC or DC electronic power supply.', 'PCB; copper; aluminium; ferrous metal; plastic housing', 'PIECE', TRUE, FALSE, FALSE, TRUE),
    ('Electrical Equipment', 'Power Equipment', 'Inverter Equipment', 'Inverter Equipment', 'Electrical inverter or associated equipment.', 'battery; PCB; copper; aluminium; ferrous metal; plastic housing', 'PIECE', TRUE, TRUE, FALSE, TRUE),

    ('Electronic Components / Scrap', 'Recovered Materials', 'PCB', 'Printed Circuit Board', 'Separated printed circuit board or board assembly.', 'electronic components; copper; aluminium; ferrous metal', 'KG', TRUE, FALSE, FALSE, TRUE),
    ('Electronic Components / Scrap', 'Recovered Materials', 'Electronic Components', 'Electronic Components', 'Separated electronic components from e-waste.', 'copper; aluminium; electronic components; mixed e-waste', 'KG', TRUE, FALSE, FALSE, TRUE),
    ('Electronic Components / Scrap', 'Recovered Materials', 'Copper Cable', 'Copper Cable', 'Separated insulated or uninsulated copper cable.', 'copper; plastic housing', 'KG', TRUE, FALSE, FALSE, TRUE),
    ('Electronic Components / Scrap', 'Recovered Materials', 'Aluminium', 'Aluminium', 'Separated aluminium recovered from equipment.', 'aluminium; mixed e-waste', 'KG', FALSE, FALSE, FALSE, TRUE),
    ('Electronic Components / Scrap', 'Recovered Materials', 'Ferrous Metal', 'Ferrous Metal', 'Separated iron or steel recovered from equipment.', 'ferrous metal; mixed e-waste', 'KG', FALSE, FALSE, FALSE, TRUE),
    ('Electronic Components / Scrap', 'Recovered Materials', 'Battery', 'Battery', 'Separated rechargeable or other electrical battery.', 'battery; electronic components', 'KG', TRUE, TRUE, FALSE, TRUE),
    ('Electronic Components / Scrap', 'Recovered Materials', 'Plastic Housing', 'Plastic Housing', 'Separated plastic enclosure from electronic equipment.', 'plastic housing; mixed e-waste', 'KG', FALSE, FALSE, FALSE, TRUE),
    ('Electronic Components / Scrap', 'Recovered Materials', 'Mixed E-Waste', 'Mixed E-Waste', 'Unsorted electronic and electrical equipment or components.', 'PCB; battery; copper; aluminium; ferrous metal; plastic housing; mixed e-waste', 'KG', TRUE, TRUE, FALSE, TRUE);
