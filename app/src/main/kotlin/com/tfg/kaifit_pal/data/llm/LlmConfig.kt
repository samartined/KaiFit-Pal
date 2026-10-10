package com.tfg.kaifit_pal.data.llm

/** Limits and sampling of the language model and of a Kai-Q conversation. */
object LlmConfig {

    /** Context window of the engine (KV cache), in tokens: prompt, history and reply together. */
    const val MAX_NUM_TOKENS = 4096

    /** Longest reply, in tokens. */
    const val MAX_OUTPUT_TOKENS = 1024

    /** Sampling: the 40 most likely tokens, nucleus 0.95, low temperature for steady answers. */
    const val TOP_K = 40
    const val TOP_P = 0.95
    const val TEMPERATURE = 0.3

    /**
     * Tokens after which the conversation is full: 4096 − 1024 for a whole reply − 512 for the
     * next user message.
     */
    const val CONTEXT_FULL_TOKENS = 2560

    /** User messages after which the conversation is full, whatever its tokens. */
    const val MAX_USER_TURNS = 20

    /** Longest user message, in characters. */
    const val MAX_INPUT_CHARS = 1000
}
