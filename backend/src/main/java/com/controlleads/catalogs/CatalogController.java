package com.controlleads.catalogs;

import com.controlleads.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-managed catalogs (module_settings.md). Reads are open to any
 * authenticated user (form dropdowns); writes are ADMINISTRATOR only.
 */
@RestController
@Tag(name = "catalogs")
public class CatalogController {

    public record CatalogDto(UUID id, String name, boolean active) {
        static CatalogDto from(CatalogItem item) {
            return new CatalogDto(item.getId(), item.getName(), item.isActive());
        }
    }

    public record CountryDto(String code, String name) {}

    public record CreateCatalogRequest(@NotBlank String name) {}
    public record UpdateCatalogRequest(String name, Boolean active) {}

    @Operation(summary = "List ISO countries for dropdowns")
    @GetMapping("/api/countries")
    public List<CountryDto> listCountries() {
        return java.util.Arrays.stream(java.util.Locale.getISOCountries())
                .map(code -> new CountryDto(code, new java.util.Locale("", code).getDisplayCountry(java.util.Locale.ENGLISH)))
                .filter(c -> c.name() != null && !c.name().isBlank())
                .sorted(java.util.Comparator.comparing(CountryDto::name))
                .toList();
    }

    private final CourseRepository courses;
    private final ChannelRepository channels;
    private final StallReasonRepository stallReasons;
    private final CampaignRepository campaigns;

    public CatalogController(CourseRepository courses, ChannelRepository channels,
                             StallReasonRepository stallReasons, CampaignRepository campaigns) {
        this.courses = courses;
        this.channels = channels;
        this.stallReasons = stallReasons;
        this.campaigns = campaigns;
    }

    // ----- Campaigns -----

    @Operation(summary = "List campaigns")
    @GetMapping("/api/campaigns")
    public List<CatalogDto> listCampaigns() {
        return list(campaigns);
    }

    @Operation(summary = "Create campaign (admin)")
    @PostMapping("/api/campaigns")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDto createCampaign(@Valid @RequestBody CreateCatalogRequest request) {
        if (campaigns.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("Campaign already exists");
        }
        return CatalogDto.from(campaigns.save(new Campaign(request.name())));
    }

    @Operation(summary = "Rename or (de)activate campaign (admin)")
    @PatchMapping("/api/campaigns/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public CatalogDto updateCampaign(@PathVariable UUID id, @RequestBody UpdateCatalogRequest request) {
        return update(campaigns, id, request);
    }

    // ----- Courses -----

    @Operation(summary = "List courses")
    @GetMapping("/api/courses")
    public List<CatalogDto> listCourses() {
        return list(courses);
    }

    @Operation(summary = "Create course (admin)")
    @PostMapping("/api/courses")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDto createCourse(@Valid @RequestBody CreateCatalogRequest request) {
        if (courses.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("Course already exists");
        }
        return CatalogDto.from(courses.save(new Course(request.name())));
    }

    @Operation(summary = "Rename or (de)activate course (admin)")
    @PatchMapping("/api/courses/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public CatalogDto updateCourse(@PathVariable UUID id, @RequestBody UpdateCatalogRequest request) {
        return update(courses, id, request);
    }

    // ----- Channels -----

    @Operation(summary = "List channels")
    @GetMapping("/api/channels")
    public List<CatalogDto> listChannels() {
        return list(channels);
    }

    @Operation(summary = "Create channel (admin)")
    @PostMapping("/api/channels")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDto createChannel(@Valid @RequestBody CreateCatalogRequest request) {
        if (channels.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("Channel already exists");
        }
        return CatalogDto.from(channels.save(new Channel(request.name())));
    }

    @Operation(summary = "Rename or (de)activate channel (admin)")
    @PatchMapping("/api/channels/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public CatalogDto updateChannel(@PathVariable UUID id, @RequestBody UpdateCatalogRequest request) {
        return update(channels, id, request);
    }

    // ----- Stall reasons -----

    @Operation(summary = "List stall reasons")
    @GetMapping("/api/stall-reasons")
    public List<CatalogDto> listStallReasons() {
        return list(stallReasons);
    }

    @Operation(summary = "Create stall reason (admin)")
    @PostMapping("/api/stall-reasons")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDto createStallReason(@Valid @RequestBody CreateCatalogRequest request) {
        if (stallReasons.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("Stall reason already exists");
        }
        return CatalogDto.from(stallReasons.save(new StallReason(request.name())));
    }

    @Operation(summary = "Rename or (de)activate stall reason (admin)")
    @PatchMapping("/api/stall-reasons/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public CatalogDto updateStallReason(@PathVariable UUID id, @RequestBody UpdateCatalogRequest request) {
        return update(stallReasons, id, request);
    }

    // ----- Shared helpers -----

    private <T extends CatalogItem> List<CatalogDto> list(JpaRepository<T, UUID> repo) {
        return repo.findAll(Sort.by("name")).stream().map((Function<T, CatalogDto>) CatalogDto::from).toList();
    }

    private <T extends CatalogItem> CatalogDto update(JpaRepository<T, UUID> repo, UUID id,
                                                      UpdateCatalogRequest request) {
        T item = repo.findById(id).orElseThrow(() -> ApiException.notFound("Catalog item not found"));
        if (request.name() != null && !request.name().isBlank()) item.setName(request.name());
        if (request.active() != null) item.setActive(request.active());
        return CatalogDto.from(item);
    }
}
