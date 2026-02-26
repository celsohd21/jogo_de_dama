package main;

import javax.swing.*;
import java.awt.*;

/**
 * @author Douglas
 */
public final class MainInterfaceGrafica extends JFrame {

    private final int TAMANHO = 6;
    private final CasaBotao[][] tabuleiroInterface = new CasaBotao[TAMANHO][TAMANHO];
    
    /*
        Vazio: 0
        Brancas: 1
        Pretas: 2
        Damas: 3 (branca) ou 4 (preta)
    
        -> REGRAS DO JOGO

            - DEFINIR QUEM UTILIZARÁ AS PEÇAS BRANCAS (COMEÇA O JOGO)
            - OBRIGATÓRIO COMER A PEÇA
            - NÃO É PERMITIDO COMER PRA TRÁS
            - UMA PEÇA PODE COMER MÚLTIPLAS PEÇAS, EM QUALQUER
            DIREÇÃO, DESDE QUE A PRIMEIRA SEJA PARA FRENTE
            - A DAMA PODE ANDAR INFINITAS CASAS, RESPEITANDO O LIMITE DO TABULEIRO
            - A DAMA PODE COMER PRA TRÁS
            - A DAMA PODE COMER MÚLTIPLAS PEÇAS
            - A ÚLTIMA PEÇA A SER COMIDA PELA DAMA 
            INDICA A POSIÇÃO QUE A DAMA DEVERÁ PARAR 
            (POSIÇÃO SUBSEQUENTE NA DIREÇÃO DA COMIDA)
            - NA IMPOSSIBILIDADE DE EFETUAR JOGADAS, 
            O JOGADOR TRAVADO PERDE O JOGO
    */
    private final Tabuleiro tabuleiroLogico; 
    private int linhaOrigem = -1, colOrigem = -1;

    public MainInterfaceGrafica() {
        
        /*
            TABULEIRO DO JOGO
        */
        tabuleiroLogico = new Tabuleiro();

        setTitle("DISCIPLINA - IA - MINI JOGO DE DAMA");
        setSize(800, 800);
        setLayout(new GridLayout(TAMANHO, TAMANHO));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        inicializarComponentes();
        sincronizarInterface(); 

        setVisible(true);
    }

    private void inicializarComponentes() {
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) {
                tabuleiroInterface[i][j] = new CasaBotao();

                // Cores do tabuleiro
                if ((i + j) % 2 == 0) {
                    tabuleiroInterface[i][j].setBackground(new Color(235, 235, 208)); // Bege
                } else {
                    tabuleiroInterface[i][j].setBackground(new Color(119, 149, 86));  // Verde
                }

                int linha = i;
                int coluna = j;
                tabuleiroInterface[i][j].addActionListener(e -> tratarClique(linha, coluna));
                add(tabuleiroInterface[i][j]);
            }
        }
    }

    private void tratarClique(int linha, int col) {
        
        // Caso 1: Nenhuma peça selecionada ainda
        if (linhaOrigem == -1) {
            
            // Verifica se a casa clicada contém QUALQUER peça (1, 2, 3 ou 4)
            if (tabuleiroLogico.getMatriz()[linha][col] != 0) {
                linhaOrigem = linha;
                colOrigem = col;
                tabuleiroInterface[linha][col].setBackground(Color.YELLOW); // Destaque do clique
            }
        } 
        // Caso 2: Já existe uma peça selecionada, tentando mover
        else {
            
            // Se clicar na mesma peça, cancela a seleção
            if (linhaOrigem == linha && colOrigem == col) {
                cancelarSelecao();
                return;
            }

            boolean sucesso = moverPecaLogica(linhaOrigem, colOrigem, linha, col);
            
            if (sucesso) {
                cancelarSelecao();
                sincronizarInterface();

                /*
                    VERIFICAÇÃO DE QUEM É A VEZ DE JOGAR E IMPLEMENTAÇÃO DA JOGADA DA IA
                */
                
                
            } else {
                // Se o movimento for inválido (ex: clicar em cima de outra peça)
                cancelarSelecao();
            }
        }
    }

    private void cancelarSelecao() {
        if (linhaOrigem != -1) {
            // Restaura a cor original
            tabuleiroInterface[linhaOrigem][colOrigem].setBackground(new Color(119, 149, 86));
        }
        linhaOrigem = -1;
        colOrigem = -1;
    }

    private boolean moverPecaLogica(int r1, int c1, int r2, int c2) {
        
        // A casa de destino deve estar vazia
        if (tabuleiroLogico.getMatriz()[r2][c2] == 0) {
            
            // Transfere o valor (seja 1, 2, 3 ou 4) para a nova posição
            tabuleiroLogico.getMatriz()[r2][c2] = tabuleiroLogico.getMatriz()[r1][c1];
            tabuleiroLogico.getMatriz()[r1][c1] = 0;

            // Promoção simples para Dama (opcional)
            if (tabuleiroLogico.getMatriz()[r2][c2] == 2 && r2 == 5) {
                tabuleiroLogico.getMatriz()[r2][c2] = 4;
            }
            if (tabuleiroLogico.getMatriz()[r2][c2] == 1 && r2 == 0) {
                tabuleiroLogico.getMatriz()[r2][c2] = 3;
            }
            
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainInterfaceGrafica::new);
    }
    
    /*
     * Atualiza a interface gráfica com base na matriz lógica do Tabuleiro. Este
     * método será chamado após cada jogada da IA.
     */
    public void sincronizarInterface() {
        for (int i = 0; i < TAMANHO; i++) {
            for (int j = 0; j < TAMANHO; j++) {
                int peca = tabuleiroLogico.getMatriz()[i][j];
                tabuleiroInterface[i][j].setTipoPeca(peca);
            }
        }
    }

    private class CasaBotao extends JButton {

        private int tipoPeca = 0;

        public void setTipoPeca(int tipo) {
            this.tipoPeca = tipo;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int margem = 10;
            // Brancas
            if (tipoPeca == 1 || tipoPeca == 3) { 
                g2.setColor(Color.WHITE);
                g2.fillOval(margem, margem, getWidth() - 2 * margem, getHeight() - 2 * margem);
                g2.setColor(Color.BLACK);
                g2.drawOval(margem, margem, getWidth() - 2 * margem, getHeight() - 2 * margem);
            // Pretas
            } else if (tipoPeca == 2 || tipoPeca == 4) { 
                g2.setColor(Color.BLACK);
                g2.fillOval(margem, margem, getWidth() - 2 * margem, getHeight() - 2 * margem);
            }

            // Representação de Dama (uma borda dourada)
            if (tipoPeca > 2) { 
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(margem + 5, margem + 5, getWidth() - 2 * margem - 10, getHeight() - 2 * margem - 10);
            }
        }
    }
}