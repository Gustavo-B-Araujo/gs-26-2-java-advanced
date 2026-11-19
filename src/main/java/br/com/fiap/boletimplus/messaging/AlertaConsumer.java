package br.com.fiap.boletimplus.messaging;

import br.com.fiap.boletimplus.config.RabbitConfig;
import br.com.fiap.boletimplus.model.AlunoGuardiao;
import br.com.fiap.boletimplus.repository.AlunoGuardiaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consome os alertas da fila e decide quais responsáveis avisar (AlunoGuardiao).
 * O aviso é registrado no log; um canal real (push, e-mail, WhatsApp) entraria aqui.
 */
@Component
public class AlertaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertaConsumer.class);

    private final AlunoGuardiaoRepository vinculos;

    public AlertaConsumer(AlunoGuardiaoRepository vinculos) {
        this.vinculos = vinculos;
    }

    @Transactional(readOnly = true)
    @RabbitListener(queues = RabbitConfig.FILA_ALERTAS)
    public void aoReceber(AlertaDesempenhoEvent evento) {
        List<AlunoGuardiao> responsaveis = vinculos.findByAlunoCod(evento.alunoCod());
        if (responsaveis.isEmpty()) {
            log.info("Alerta de {} em {}: nenhum responsável vinculado.", evento.alunoNome(), evento.materia());
            return;
        }
        for (AlunoGuardiao v : responsaveis) {
            log.info("Aviso para {} ({}): {} está em atenção em {} — {}.",
                    v.getGuardiao().getPessoa().getNome(),
                    v.getTipoParentesco().getEnumerador(),
                    evento.alunoNome(),
                    evento.materia(),
                    evento.motivo());
        }
    }
}
