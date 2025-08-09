package ru.netology.yandexmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.yandexmap.dto.Post


class ItemFragment : Fragment() {

    private var postList = emptyList<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val gson = Gson()
            val token = TypeToken.getParameterized(List::class.java, Post::class.java).type
            val string = it.getString("KEY_MAIN_TO_LIST")
            if (!string.isNullOrBlank()) {
                postList = gson.fromJson(string, token)
                println(postList)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = MyItemRecyclerViewAdapter(postList, object : OnInteractionListener {
                    override fun onLike(post: Post) {
                        super.onLike(post)
                        println("LIKED   $post")
                        val gson = Gson()
                        val bundle = Bundle()
                        bundle.putSerializable("KEY_LIST_TO_MAIN", gson.toJson(post))

                        findNavController().navigate(R.id.action_itemFragment_to_mainFragment2, bundle)
                    }
                })
            }
        }
        return view
    }

}