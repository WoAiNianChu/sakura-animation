package com.jing.sakura.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import androidx.navigation.fragment.findNavController
import coil.load
import com.jing.sakura.data.AnimeDetailPageData
import com.jing.sakura.data.AnimePlayListEpisode
import com.jing.sakura.data.Resource
import com.jing.sakura.databinding.DetailInfomationLayoutBinding
import com.jing.sakura.extend.observeLiveData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnimeDetailFragment : RowsSupportFragment() {

    private val viewModel by viewModels<DetailPageViewModel>()

    private lateinit var detailPageUrl: String
    private var animeName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        detailPageUrl = AnimeDetailFragmentArgs.fromBundle(requireArguments()).detailUrl
        viewModel.loadData(detailPageUrl)
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val episode = item as AnimePlayListEpisode
            findNavController().navigate(
                AnimeDetailFragmentDirections.actionAnimeDetailFragmentToLoadVideoUrlFragment(
                    item.url,
                    "$animeName - ${episode.episode}"
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeLiveData(viewModel.detailPageData) {
            when (it) {
                is Resource.Success -> {
                    animeName = it.data.animeName
                    renderData(it.data)
                }
                else -> {}
            }
        }
    }

    private fun renderData(detailPageData: AnimeDetailPageData) {
        val rowsAdapter = createRowsAdapter()

        rowsAdapter.add(detailPageData)
        detailPageData.playLists.forEachIndexed { index, playlist ->
            val episodeAdapter = ArrayObjectAdapter(DetailEpisodePresenter())
            playlist.episodeList.forEach(episodeAdapter::add)
            val listRow = ListRow(HeaderItem("播放列表${index + 1}"), episodeAdapter)
            rowsAdapter.add(listRow)
        }


        adapter = rowsAdapter
    }


    private fun createRowsAdapter(): ArrayObjectAdapter {
        val rowPresenterSelector = ClassPresenterSelector()
        rowPresenterSelector.addClassPresenter(
            AnimeDetailPageData::class.java,
            DetailInfoRowPresenter().apply {
                this.selectEffectEnabled = false
            }
        )
        rowPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        return ArrayObjectAdapter(rowPresenterSelector)
    }
}


class DetailInfoRowPresenter : RowPresenter() {


    init {
        headerPresenter = null
    }

    override fun createRowViewHolder(parent: ViewGroup?): ViewHolder {
        val ctx = parent!!.context
        val binding = DetailInfomationLayoutBinding.inflate(LayoutInflater.from(ctx))
        binding.root.tag = binding
        return ViewHolder(binding.root)
    }

    override fun onBindRowViewHolder(vh: ViewHolder?, item: Any?) {
        val detail = item as AnimeDetailPageData
        with(vh!!.view.tag as DetailInfomationLayoutBinding) {
            detailAlias.text = detail.animeAlias
            detailName.text = detail.animeName
            detailCoverImg.load(detail.imageUrl)
            detailRegion.text = detail.region.name
            detailRelease.text = detail.releaseDay
            detailTags.text = detail.tags.joinToString("  ") { it.name }
            detailCurrentEpisode.text = detail.latestEpisode
        }
    }

}
