/**
 * Copyright (c) 2021, the WikiOIE AUTHORS.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the University of Bari nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * GNU GENERAL PUBLIC LICENSE - Version 3, 29 June 2007
 *
 */

package di.uniba.it.wikioie.udp;

import di.uniba.it.wikioie.data.Token;
import java.util.List;
import org.jgrapht.Graph;

/**
 *
 * @author pierpaolo
 */
public class UDPSentence {
    
    private String id;
    
    private String text;
    
    private String conll;
    
    private List<Token> tokens;
    
    private Graph<Token, String> graph;

    /**
     *
     */
    public UDPSentence() {
    }

    /**
     *
     * @param id
     */
    public UDPSentence(String id) {
        this.id = id;
    }  

    /**
     *
     * @param id
     * @param text
     * @param conll
     */
    public UDPSentence(String id, String text, String conll) {
        this.id = id;
        this.text = text;
        this.conll = conll;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     */
    public String getText() {
        return text;
    }

    /**
     *
     * @param text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     *
     * @return
     */
    public String getConll() {
        return conll;
    }

    /**
     *
     * @param conll
     */
    public void setConll(String conll) {
        this.conll = conll;
    }

    /**
     *
     * @return
     */
    public List<Token> getTokens() {
        return tokens;
    }

    /**
     *
     * @param tokens
     */
    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     *
     * @return
     */
    public Graph<Token, String> getGraph() {
        return graph;
    }

    /**
     *
     * @param graph
     */
    public void setGraph(Graph<Token, String> graph) {
        this.graph = graph;
    }
    
}
