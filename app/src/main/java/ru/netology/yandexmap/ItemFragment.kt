package ru.netology.yandexmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.yandexmap.databinding.FragmentItemListBinding
import ru.netology.yandexmap.databinding.FragmentMainBinding
import ru.netology.yandexmap.dto.Marker
import ru.netology.yandexmap.viewmodel.YaMapViewModel
import kotlin.getValue


class ItemFragment : Fragment() {

//    private var markerList = emptyList<Marker>()

    val viewModel by activityViewModels<YaMapViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        arguments?.let {
//            val gson = Gson()
//            val token = TypeToken.getParameterized(List::class.java, Marker::class.java).type
//            val string = it.getString("KEY_MAIN_TO_LIST")
//            if (!string.isNullOrBlank()) {
//                markerList = gson.fromJson(string, token)
//                println(markerList)
//            }
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentItemListBinding.inflate(inflater, container, false)
        val markerList = viewModel.data.value ?: emptyList()

        val adapter = MyItemRecyclerViewAdapter(
            markerList,
            object : OnInteractionListener {
                override fun onMarker(marker: Marker) {
                    super.onMarker(marker)
                    println("LIKED   $marker")
                    val gson = Gson()
                    val bundle = Bundle()
                    bundle.putSerializable("KEY_LIST_TO_MAIN", gson.toJson(marker))

                    findNavController().navigate(R.id.action_itemFragment_to_mainFragment2, bundle)
                }
            }
        )

        binding.list.adapter = adapter

        viewModel.data.observe(viewLifecycleOwner) { posts ->
            adapter.notifyDataSetChanged()
        }

//        val view = inflater.inflate(R.layout.fragment_item_list, container, false)
// Set the adapter
//        if (view is RecyclerView) {
//            with(view) {
//                layoutManager = LinearLayoutManager(context)
//                adapter = MyItemRecyclerViewAdapter(markerList, object : OnInteractionListener {
//                    override fun onMarker(marker: Marker) {
//                        super.onMarker(marker)
//                        println("LIKED   $marker")
//                        val gson = Gson()
//                        val bundle = Bundle()
//                        bundle.putSerializable("KEY_LIST_TO_MAIN", gson.toJson(marker))
//
//                        findNavController().navigate(R.id.action_itemFragment_to_mainFragment2, bundle)
//                    }
//                })
//            }
//        }
//        return view
        return binding.root
    }
}