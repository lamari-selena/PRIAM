-- Ne pas l'inclure dans un environnement de production
use `priam-data`

INSERT INTO `data` VALUES
(1, 12, 'po_ADDRESS1', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
(2, 12, 'po_ADDRESS2', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
(3, 12, 'po_ADDRESSNAME', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
(4, 12, 'po_CREADITCARDCOMPANY', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
(5, 12, 'po_CREADITCARDNUMBER', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
(6, 12, 'po_CREADITCARDEXPIREDATE', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
(7, 12, 'po_EMAIL', 1, b'1', b'1', b'1', 0, NULL, 6, 2);

INSERT INTO `processing` VALUES
(1, '2024-05-10 00:28:30', '2024-05-23 00:28:30', 'CONSENT', 'recommender', 3),
(2, '2024-05-10 00:28:30', '2024-05-23 00:28:30', 'CONSENT', 'place an order', 1);


INSERT INTO `data_usage` VALUES
(1, b'0', b'0', 1, b'1', b'1', b'0', 1),
(2, b'0', b'0', 2, b'1', b'1', b'0', 1),
(3, b'0', b'0', 3, b'1', b'1', b'0', 1),
(4, b'0', b'0', 4, b'1', b'1', b'0', 1),
(5, b'0', b'0', 5, b'1', b'1', b'0', 1),
(6, b'0', b'0', 6, b'1', b'1', b'0', 1),
(7, b'0', b'0', 7, b'1', b'1', b'0', 1);


INSERT INTO `purpose` VALUES 
(1, 'Recommend products tailored to the user''s preferences and behaviors to provide them with a personalized shopping experience.', 0),
(2, 'passer une commande', 0);

INSERT INTO `processing_purposes` VALUES
(1, 1),
(2, 2);

INSERT INTO `consent` VALUES
(1, 1),
(1, 2);

INSERT INTO `processed_data` VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(5, 1),
(6, 1),
(7, 1);