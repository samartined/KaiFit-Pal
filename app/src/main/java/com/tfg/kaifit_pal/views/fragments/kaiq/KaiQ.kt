package com.tfg.kaifit_pal.views.fragments.kaiq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.kaimodel.GPTApiCaller

class KaiQ : Fragment() {
    private var recyclerView: RecyclerView? = null
    private var messageEditText: EditText? = null
    private var sendButton: ImageButton? = null
    private var messageList: MutableList<MessageController>? = null
    private var messageAdapter: MessageAdapter? = null

    private val gptApiCaller = GPTApiCaller(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_kai_q, container, false)

        messageList = ArrayList()

        setHasOptionsMenu(true)
        setUpComponents(view)
        setUpAdapter()

        return view
    }

    fun setUpComponents(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
        messageEditText = view.findViewById(R.id.message_edit_text)
        sendButton = view.findViewById(R.id.send_btn)
        setUpListeners()
    }

    fun setUpListeners() {
        sendButton!!.setOnClickListener { v: View? ->
            val userQuery = messageEditText!!.text.toString().trim { it <= ' ' }
            if (!userQuery.isEmpty()) {
                addToChat(userQuery, MessageController.Companion.SENT_BY_USER)
                messageEditText!!.setText("")
                gptApiCaller.gptApiRequest(userQuery)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.kai_q_appbar, menu)
        val restoreItem = menu.findItem(R.id.action_restore)
        restoreItem.setOnMenuItemClickListener { item: MenuItem? ->
            clearChat()
            true
        }
    }

    fun setUpAdapter() {
        messageAdapter = MessageAdapter(messageList)
        recyclerView!!.adapter = messageAdapter
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.stackFromEnd = true
        recyclerView!!.layoutManager = linearLayoutManager
    }

    fun addToChat(message: String?, sentBy: String) {
        val runnable = Runnable {
            messageList!!.add(MessageController(message, sentBy))
            messageAdapter!!.notifyDataSetChanged()
            recyclerView!!.smoothScrollToPosition(messageAdapter!!.itemCount)
        }
        requireActivity().runOnUiThread(runnable)
    }

    fun addResponseToChat(response: String?) {
        addToChat(response, MessageController.Companion.SENT_BY_BOT)
    }

    fun clearChat() {
        messageList!!.clear()
        messageAdapter!!.notifyDataSetChanged()
    }
}