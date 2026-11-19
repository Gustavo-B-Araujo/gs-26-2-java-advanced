UPDATE Usuario SET senha_hash = '$2a$10$NkTvtTP/RLUbt2ZiEIfR1euVW.iSK8ozFB1UCwXk.8L7CNTliQmE.' WHERE cod_pessoa IN (SELECT cod FROM Pessoa WHERE num_documento LIKE '1234567890_');
COMMIT;
